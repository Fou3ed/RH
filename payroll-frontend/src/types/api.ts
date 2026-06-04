/** Response shape of GET /system/info */
export interface SystemInfo {
  application: string;
  version: string;
  status: string;
  timestamp: string;
}

/** Standard error envelope returned by the backend GlobalExceptionHandler */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
