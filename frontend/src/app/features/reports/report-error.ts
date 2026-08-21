import { HttpErrorResponse } from '@angular/common/http';

export function reportErrorMessage(err: HttpErrorResponse): string {
  return messageFromBody(err.error) ?? fallbackMessage(err);
}

export function applyReportError(err: HttpErrorResponse, onMessage: (message: string) => void): void {
  if (typeof Blob !== 'undefined' && err.error instanceof Blob) {
    void err.error
      .text()
      .then((text) => onMessage(messageFromBody(parseMaybeJson(text)) ?? fallbackMessage(err)))
      .catch(() => onMessage(fallbackMessage(err)));
    return;
  }
  onMessage(reportErrorMessage(err));
}

function messageFromBody(body: unknown): string | null {
  if (body == null || (typeof Blob !== 'undefined' && body instanceof Blob)) {
    return null;
  }
  if (typeof body === 'string') {
    const trimmed = body.trim();
    if (!trimmed) {
      return null;
    }
    try {
      return messageFromBody(JSON.parse(trimmed) as unknown);
    } catch {
      return trimmed;
    }
  }
  if (typeof body === 'object' && 'message' in body) {
    const value = (body as { message?: unknown }).message;
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return null;
}

function parseMaybeJson(text: string): unknown {
  const trimmed = text.trim();
  if (!trimmed) {
    return null;
  }
  try {
    return JSON.parse(trimmed) as unknown;
  } catch {
    return trimmed;
  }
}

function fallbackMessage(err: HttpErrorResponse): string {
  if (err.status === 403) {
    return 'You do not have access to this report.';
  }
  if (err.status === 0) {
    return 'Unable to connect to the backend on this company host (port 8080).';
  }
  return 'Unable to load this report.';
}
