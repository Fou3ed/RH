#!/usr/bin/env python3
"""
Migrate employee master data from the MARAM Excel workbook into the payroll
platform, via the REST API.

Reads the ``ETAT PERSONNEL`` sheet (identity, family situation, children,
category, hire date, CNSS/CIN) and creates employees through ``POST /employees``
so all server-side validation applies. Existing employees (409) are skipped.

IMPORTANT
- This reads REAL employee PII from a local file. The workbook is git-ignored;
  do not commit it or any exported data.
- Computed values (base salary, échelon multipliers) live in workbook formulas
  that are not recalculated in the saved file (#REF!/#N/A), so base salaries are
  NOT imported here — HR enters them, or re-save the workbook from Excel first.

Usage:
    python tools/migrate_workbook.py \
        --file "docs/prime annuel 2025.xlsm" \
        --base-url http://localhost:8080/api \
        --user admin --password 'Admin@123!' [--dry-run] [--limit N]

Requires: openpyxl  (pip install openpyxl)
"""
import argparse
import datetime as dt
import json
import sys
import urllib.error
import urllib.request

import openpyxl

# ETAT PERSONNEL column indices (1-based), data starts at row 3.
COL_MLE = 2
COL_NAME = 3
COL_GENRE = 4
COL_ETAT_CIVIL = 5
COL_CHEF_FAM = 6
COL_CHILDREN = 7
COL_CIN = 8
COL_CNSS = 10
COL_CONTRAT = 13
COL_CAT = 16
COL_HIRE = 17
DATA_START_ROW = 3
DEFAULT_HIRE = "2000-01-01"


def post(url, payload, token):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def get(url, token):
    req = urllib.request.Request(url, method="GET")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(base_url, user, password):
    res = post(f"{base_url}/auth/login", {"username": user, "password": password}, None)
    return res["token"]


def to_iso_date(value):
    if value is None or value == "":
        return None
    if isinstance(value, (dt.datetime, dt.date)):
        return value.date().isoformat() if isinstance(value, dt.datetime) else value.isoformat()
    text = str(value).strip()
    for fmt in ("%d/%m/%Y", "%Y-%m-%d", "%d-%m-%Y"):
        try:
            return dt.datetime.strptime(text, fmt).date().isoformat()
        except ValueError:
            continue
    return None


def cell(row, idx):
    v = row[idx - 1]
    return ("" if v is None else v)


def family_status(etat_civil, chef_fam, children):
    etat = str(etat_civil).lower()
    if "céli" in etat or "celi" in etat or "single" in etat:
        return "C"
    if str(chef_fam).strip().lower() in ("oui", "yes", "o"):
        return "M" + str(min(children, 4))
    return "C"  # married but not head-of-household → no chef-de-famille abatement


def split_name(full):
    tokens = [t for t in str(full).split() if t.strip()]
    if not tokens:
        return None, None
    if len(tokens) == 1:
        return tokens[0], "-"
    return tokens[0], " ".join(tokens[1:])  # workbook lists FAMILY name first


def build_payload(row, department_id):
    mle = cell(row, COL_MLE)
    name = cell(row, COL_NAME)
    if not str(mle).strip() or not str(name).strip():
        return None

    last, first = split_name(name)
    if not last:
        return None

    children = 0
    try:
        children = int(cell(row, COL_CHILDREN) or 0)
    except (TypeError, ValueError):
        children = 0

    genre = str(cell(row, COL_GENRE)).strip().lower()
    gender = "H" if genre.startswith("h") else "M"  # Homme→H, Femme→M (matches [MH] pattern)

    hire = to_iso_date(cell(row, COL_HIRE)) or DEFAULT_HIRE

    cat = cell(row, COL_CAT)
    try:
        echelon = max(1, min(14, int(float(cat))))
    except (TypeError, ValueError):
        echelon = 1

    payload = {
        "employeeId": str(mle).strip().replace(".0", ""),
        "firstName": first,
        "lastName": last,
        "hireDate": hire,
        "departmentId": department_id,
        "categoryId": 1,          # CAT1 — refine once category mapping is confirmed with HR
        "echelon": echelon,
        "gender": gender,
        "familyStatus": family_status(cell(row, COL_ETAT_CIVIL), cell(row, COL_CHEF_FAM), children),
        "numberOfChildren": children,
    }
    cin = str(cell(row, COL_CIN)).strip().replace(".0", "")
    if cin and cin not in ("0", "None"):
        payload["nationalId"] = cin[:20]
    cnss = str(cell(row, COL_CNSS)).strip()
    if cnss and cnss not in ("0", "None", ""):
        payload["cnssNumber"] = cnss[:20]
    return payload


def main():
    ap = argparse.ArgumentParser(description="Import employees from the MARAM workbook")
    ap.add_argument("--file", required=True)
    ap.add_argument("--base-url", default="http://localhost:8080/api")
    ap.add_argument("--user", default="admin")
    ap.add_argument("--password", default="Admin@123!")
    ap.add_argument("--sheet", default="ETAT PERSONNEL")
    ap.add_argument("--limit", type=int, default=0, help="max rows to import (0 = all)")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    wb = openpyxl.load_workbook(args.file, data_only=True, read_only=True)
    ws = wb[args.sheet]

    token = None
    department_id = 1
    if not args.dry_run:
        token = login(args.base_url, args.user, args.password)
        depts = get(f"{args.base_url}/departments", token)
        gen = next((d for d in depts if d.get("code") == "GEN"), depts[0] if depts else None)
        if not gen:
            print("No department found — create one first.", file=sys.stderr)
            sys.exit(1)
        department_id = gen["id"]

    created = skipped = errors = 0
    for i, row in enumerate(ws.iter_rows(min_row=DATA_START_ROW, values_only=True), DATA_START_ROW):
        payload = build_payload(row, department_id)
        if payload is None:
            continue
        if args.dry_run:
            print(json.dumps(payload, ensure_ascii=False))
            created += 1
        else:
            try:
                post(f"{args.base_url}/employees", payload, token)
                created += 1
            except urllib.error.HTTPError as e:
                if e.code == 409:
                    skipped += 1
                else:
                    errors += 1
                    print(f"row {i} ({payload['employeeId']}): HTTP {e.code} {e.read().decode()[:200]}",
                          file=sys.stderr)
        if args.limit and created >= args.limit:
            break

    print(f"\nDone. created/previewed={created}, skipped(exists)={skipped}, errors={errors}")


if __name__ == "__main__":
    main()
