import { beforeEach, describe, expect, it } from 'vitest';
import { tokenStorage } from '@/services/tokenStorage';
import type { AuthUser } from '@/types/auth';

const sampleUser: AuthUser = {
  id: 1,
  username: 'sonia',
  email: 'sonia@maram.tn',
  employeeId: 25,
  status: 'ACTIVE',
  roles: ['HR_MANAGER'],
  permissions: ['employee.view', 'attendance.record'],
};

describe('tokenStorage', () => {
  beforeEach(() => localStorage.clear());

  it('stores and retrieves access + refresh tokens', () => {
    tokenStorage.setTokens('access-1', 'refresh-1');
    expect(tokenStorage.getAccessToken()).toBe('access-1');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-1');
  });

  it('serialises and restores the user object', () => {
    tokenStorage.setUser(sampleUser);
    const restored = tokenStorage.getUser();
    expect(restored?.username).toBe('sonia');
    expect(restored?.roles).toContain('HR_MANAGER');
    expect(restored?.permissions).toContain('attendance.record');
  });

  it('returns null when no user is stored', () => {
    expect(tokenStorage.getUser()).toBeNull();
  });

  it('clear() removes tokens and the user', () => {
    tokenStorage.setTokens('a', 'r');
    tokenStorage.setUser(sampleUser);
    tokenStorage.clear();
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(tokenStorage.getUser()).toBeNull();
  });
});
