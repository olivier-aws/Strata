function abs(x: number): number {
  if (x >= 0) {
    return x;
  } else {
    return -x;
  }
}

function max(a: number, b: number): number {
  if (a >= b) {
    return a;
  } else {
    return b;
  }
}

function sum(n: number): number {
  let s: number = 0;
  let i: number = 0;
  while (i < n) {
    i = i + 1;
    s = s + i;
  }
  return s;
}
