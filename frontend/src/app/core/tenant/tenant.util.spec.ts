import { apiBaseUrlForHost, parseCompanyDomainInput, parseTenantSlug, companyPortalLoginUrl } from './tenant.util';

describe('parseTenantSlug', () => {
  it('reads acme from acme.localhost', () => {
    expect(parseTenantSlug('acme.localhost')).toBe('acme');
  });

  it('normalizes Acme.MyHR.com', () => {
    expect(parseTenantSlug('Acme.MyHR.com')).toBe('acme');
  });

  it('rejects apex, reserved, and bare localhost', () => {
    expect(parseTenantSlug('localhost')).toBeNull();
    expect(parseTenantSlug('127.0.0.1')).toBeNull();
    expect(parseTenantSlug('myhr.com')).toBeNull();
    expect(parseTenantSlug('www.myhr.com')).toBeNull();
    expect(parseTenantSlug('api.myhr.com')).toBeNull();
  });
});

describe('parseCompanyDomainInput', () => {
  it('accepts a bare slug', () => {
    expect(parseCompanyDomainInput('Acme')).toBe('acme');
  });

  it('accepts a host or pasted URL', () => {
    expect(parseCompanyDomainInput('acme.localhost')).toBe('acme');
    expect(parseCompanyDomainInput('http://globex.localhost:4200/login')).toBe('globex');
  });

  it('rejects reserved slugs', () => {
    expect(parseCompanyDomainInput('api')).toBeNull();
    expect(parseCompanyDomainInput('localhost')).toBeNull();
  });
});

describe('companyPortalLoginUrl', () => {
  it('routes localhost to slug.localhost keeping the UI port', () => {
    expect(
      companyPortalLoginUrl('acme', { protocol: 'http:', hostname: 'localhost', port: '4200' })
    ).toBe('http://acme.localhost:4200/login');
  });
});

describe('apiBaseUrlForHost', () => {
  it('uses port 8080 on localhost tenants', () => {
    expect(apiBaseUrlForHost('acme.localhost', 'http:')).toBe('http://acme.localhost:8080');
  });
});
