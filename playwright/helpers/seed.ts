// Deterministic seed helper. Use this to feed deterministic inputs to pricing flows.
export function getSeed(): number {
  const raw = process.env.SEED || '42';
  const n = Number(raw);
  return Number.isFinite(n) ? n : 42;
}

export function pseudoRandomSequence(seed: number, length: number): number[] {
  // Simple LCG for deterministic sequences (not cryptographically secure)
  let x = seed >>> 0;
  const out: number[] = [];
  for (let i = 0; i < length; i++) {
    x = (1664525 * x + 1013904223) >>> 0;
    out.push(x / 0xffffffff);
  }
  return out;
}
