function check_simple(x: number): number {
  let y: number = x + 1;
  console.assert(y > x);
  return y;
}
