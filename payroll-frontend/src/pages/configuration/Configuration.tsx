import { useState } from 'react';
import { Box, Tab, Tabs, Typography } from '@mui/material';
import SalaryScalesTab from '@/pages/configuration/SalaryScalesTab';
import TaxConfigTab from '@/pages/configuration/TaxConfigTab';
import AllowancesTab from '@/pages/configuration/AllowancesTab';

export default function Configuration() {
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Typography variant="h1" sx={{ fontSize: '1.75rem', mb: 2 }}>
        Payroll configuration
      </Typography>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label="Salary scales" />
        <Tab label="Tax" />
        <Tab label="Allowances" />
      </Tabs>
      {tab === 0 && <SalaryScalesTab />}
      {tab === 1 && <TaxConfigTab />}
      {tab === 2 && <AllowancesTab />}
    </Box>
  );
}
