function abs(x: number): number {
  if (x >= 0) {
    return x;
  } else {
    return -x;
  }
}

console.assert(abs(5) === 5);
console.assert(abs(-3) === 3);
console.assert(abs(0) === 0);
