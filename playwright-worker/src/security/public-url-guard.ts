import { lookup } from "node:dns/promises";
import { isIP } from "node:net";

const DNS_CACHE_TTL_MS = 5_000;

interface CachedLookup {
  expiresAt: number;
  validation: Promise<void>;
}

export class PublicUrlGuard {
  private readonly cache = new Map<string, CachedLookup>();

  async assertAllowed(input: string): Promise<URL> {
    let url: URL;

    try {
      url = new URL(input);
    } catch {
      throw new Error(`Invalid URL: ${input}`);
    }

    if (!['http:', 'https:'].includes(url.protocol)) {
      throw new Error(`Blocked URL protocol: ${url.protocol}`);
    }

    if (url.username || url.password) {
      throw new Error('URLs containing credentials are not allowed');
    }

    const hostname = url.hostname
      .replace(/^\[/, '')
      .replace(/\]$/, '')
      .toLowerCase();

    if (
      hostname === 'localhost' ||
      hostname.endsWith('.localhost') ||
      hostname.endsWith('.local') ||
      hostname.endsWith('.internal') ||
      hostname.endsWith('.home.arpa')
    ) {
      throw new Error(`Blocked private hostname: ${hostname}`);
    }

    await this.assertPublicHost(hostname);

    return url;
  }

  private async assertPublicHost(hostname: string): Promise<void> {
    const cached = this.cache.get(hostname);

    if (cached && cached.expiresAt > Date.now()) {
      return cached.validation;
    }

    const validation = this.resolveAndValidate(hostname);

    this.cache.set(hostname, {
      expiresAt: Date.now() + DNS_CACHE_TTL_MS,
      validation,
    });

    try {
      await validation;
    } catch (error) {
      this.cache.delete(hostname);
      throw error;
    }
  }

  private async resolveAndValidate(hostname: string): Promise<void> {
    const addresses = isIP(hostname)
      ? [{ address: hostname }]
      : await lookup(hostname, {
          all: true,
          verbatim: true,
        });

    if (addresses.length === 0) {
      throw new Error(`Hostname did not resolve: ${hostname}`);
    }

    for (const { address } of addresses) {
      if (isBlockedIp(address)) {
        throw new Error(
          `Blocked non-public address for ${hostname}: ${address}`,
        );
      }
    }
  }
}

function isBlockedIp(address: string): boolean {
  const version = isIP(address);

  if (version === 4) {
    return isBlockedIpv4(address);
  }

  if (version === 6) {
    return isBlockedIpv6(address);
  }

  return true;
}

function isBlockedIpv4(address: string): boolean {
  const octets = address.split('.').map(Number);

  if (octets.length !== 4 || octets.some((value) => !Number.isInteger(value))) {
    return true;
  }

  const [a, b, c] = octets;

  return (
    a === 0 ||
    a === 10 ||
    a === 127 ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 0 && c === 0) ||
    (a === 192 && b === 0 && c === 2) ||
    (a === 192 && b === 168) ||
    (a === 198 && (b === 18 || b === 19)) ||
    (a === 198 && b === 51 && c === 100) ||
    (a === 203 && b === 0 && c === 113) ||
    a >= 224
  );
}

function isBlockedIpv6(address: string): boolean {
  const normalized = address.toLowerCase().split('%')[0];

  if (
    normalized === '::' ||
    normalized === '::1' ||
    normalized.startsWith('::ffff:')
  ) {
    return true;
  }

  const firstHextet = Number.parseInt(normalized.split(':')[0] || '0', 16);

  return (
    (firstHextet & 0xfe00) === 0xfc00 ||
    (firstHextet & 0xffc0) === 0xfe80 ||
    (firstHextet & 0xffc0) === 0xfec0 ||
    (firstHextet & 0xff00) === 0xff00 ||
    normalized.startsWith('2001:db8:')
  );
}
