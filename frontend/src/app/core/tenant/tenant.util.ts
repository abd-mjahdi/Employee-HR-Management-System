const RESERVED_SLUGS = new Set(['www', 'api', 'app', 'admin', 'mail', 'localhost']);
const SLUG_PATTERN = /^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$/;
const LOCAL_SUFFIX = '.localhost';
const BASE_DOMAIN = 'myhr.com';
const LOCAL_API_PORT = 8080;

export function parseTenantSlug(hostname: string | null | undefined): string | null {
  if (!hostname) {
    return null;
  }
  const host = hostname.trim().toLowerCase();
  if (host === 'localhost' || host === '127.0.0.1' || host === '[::1]' || host === '::1') {
    return null;
  }

  if (host.endsWith(LOCAL_SUFFIX)) {
    const slug = host.slice(0, -LOCAL_SUFFIX.length);
    return usableSlug(slug);
  }

  if (host === BASE_DOMAIN || host === `www.${BASE_DOMAIN}`) {
    return null;
  }
  if (host.endsWith(`.${BASE_DOMAIN}`)) {
    const slug = host.slice(0, -(BASE_DOMAIN.length + 1));
    return usableSlug(slug);
  }

  return null;
}

export function parseCompanyDomainInput(raw: string | null | undefined): string | null {
  if (!raw) {
    return null;
  }
  let value = raw.trim().toLowerCase();
  if (!value) {
    return null;
  }
  value = value.replace(/^https?:\/\//, '');
  value = value.split('/')[0] ?? value;
  value = value.split(':')[0] ?? value;
  if (!value.includes('.')) {
    return usableSlug(value);
  }
  return parseTenantSlug(value);
}

export function companyPortalHost(slug: string, currentHostname: string): string {
  const host = currentHostname.toLowerCase();
  if (host === BASE_DOMAIN || host === `www.${BASE_DOMAIN}` || host.endsWith(`.${BASE_DOMAIN}`)) {
    return `${slug}.${BASE_DOMAIN}`;
  }
  return `${slug}.localhost`;
}

export function companyPortalLoginUrl(
  slug: string,
  location: Pick<Location, 'protocol' | 'hostname' | 'port'>
): string {
  const portalHost = companyPortalHost(slug, location.hostname);
  const portPart = location.port ? `:${location.port}` : '';
  return `${location.protocol}//${portalHost}${portPart}/login`;
}

export function companyDomainSuffix(currentHostname: string): string {
  const host = currentHostname.toLowerCase();
  if (host === BASE_DOMAIN || host === `www.${BASE_DOMAIN}` || host.endsWith(`.${BASE_DOMAIN}`)) {
    return `.${BASE_DOMAIN}`;
  }
  return LOCAL_SUFFIX;
}

function usableSlug(slug: string): string | null {
  if (!slug || slug.includes('.') || RESERVED_SLUGS.has(slug) || !SLUG_PATTERN.test(slug)) {
    return null;
  }
  return slug;
}

export function apiBaseUrlForHost(hostname: string, protocol: string): string {
  const host = hostname.toLowerCase();
  if (host === 'localhost' || host.endsWith(LOCAL_SUFFIX) || host === '127.0.0.1') {
    return `${protocol}//${hostname}:${LOCAL_API_PORT}`;
  }
  return `${protocol}//${hostname}`;
}
