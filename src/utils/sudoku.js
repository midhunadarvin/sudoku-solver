import {randomGenerator} from './helpers';

export class Sudoku {
  constructor(N, initialGrid) {
    this.N = N;
    this.SRN = Math.sqrt(N);
    this.initialGrid = initialGrid;
    this.generateMatrix(this.initialGrid);
  }

  reset() {
    this.generateMatrix(this.initialGrid);
  }

  // Get Matrix
  getMatrix() {
    const newMatrix = [];
    for (let i = 0; i < this.matrix.length; i++) {
      newMatrix[i] = [];
      for (let j = 0; j < this.matrix[i].length; j++) {
        newMatrix[i][j] = {...this.matrix[i][j]};
      }
    }
    return newMatrix;
  }

  // Generate sudoku box
  generateMatrix(initialGrid) {
    if (initialGrid) {
      this.matrix = initialGrid.map((row) => {
        return row.map((col) => {
          return {value: col, type: col ? 'fix' : 'blank'};
        });
      });
    } else {
      this.matrix = new Array(this.N)
        .fill(0)
        .map(() => new Array(this.N).fill({value: 0, type: 'blank'}));
      this.fillDiagonal();
      this.solve(0, 3);
      this.removeDigits(50);
    }
  }

  // Fill the diagonal SRN number of SRN x SRN matrices
  fillDiagonal() {
    for (let i = 0; i < this.N; i = i + this.SRN) {
      // for diagonal box, start coordinates->i==j
      this.fillBox(i, i);
    }
  }

  // Fill a 3 x 3 matrix.
  fillBox(row, col) {
    let num;
    for (let i = 0; i < this.SRN; i++) {
      for (let j = 0; j < this.SRN; j++) {
        do {
          num = randomGenerator(this.N);
        } while (!this.unUsedInBox(row, col, num));

        this.matrix[row + i][col + j] = {value: num, type: 'fix'};
      }
    }
  }

  // Check if number is unused in the box
  unUsedInBox(row, col, num) {
    for (let i = 0; i < this.SRN; i++) {
      for (let j = 0; j < this.SRN; j++) {
        if (this.matrix[row + i][col + j].value === num) {
          return false;
        }
      }
    }
    return true;
  }

  getSteps() {
    this.animations = [];
    const {row, col} = this.findNextEmpty(0, 0);
    this.solve(row, col);
    return this.animations;
  }

  // A recursive function to solve remaining matrix
  solve(row, col) {
    // Reached the last row and last column
    if (row >= this.N && col >= this.N) {
      return true;
    }

    for (let num = 1; num <= this.N; num++) {
      if (this.checkIfSafe(row, col, num)) {
        this.matrix[row][col] = {value: num, type: 'fix'};
        if (this.animations) {
          this.animations.push({row, col, value: num, type: 'green'});
        }

        const next = this.findNextEmpty(row, col + 1);
        if (!next) {
          return true;
        }
        if (this.solve(next.row, next.col)) {
          return true;
        }

        this.matrix[row][col].value = 0;
        if (this.animations) {
          this.animations.push({row, col, value: 0, type: 'red'});
        }
      }
    }
    return false;
  }

  // Find next empty slot
  findNextEmpty(row, col) {
    if (col >= this.N) {
      row++;
      col = 0;
    }

    // Finished the last row
    if (row >= this.N) {
      return false;
    }

    // Reached the last row and last column
    if (row === this.N - 1 && col >= this.N) {
      return false;
    }

    while (this.matrix[row][col].value !== 0) {
      col++;
      // Reached the last row and last column
      if (row === this.N - 1 && col >= this.N) {
        return false;
      }

      if (col >= this.N) {
        row++;
        col = 0;
      }
    }
    return {row, col};
  }

  // Check if safe to put in cell
  checkIfSafe(row, col, num) {
    return (
      this.unUsedInRow(row, num) &&
      this.unUsedInCol(col, num) &&
      this.unUsedInBox(row - (row % this.SRN), col - (col % this.SRN), num)
    );
  }

  // Remove the K no. of digits
  removeDigits(K) {
    let count = K;
    while (count != 0) {
      let cellId = randomGenerator(this.N * this.N);

      let row = Math.floor(cellId / this.N);
      if (row === 9) {
        row -= 1;
      }

      let col = cellId % 9;

      // if (col != 0)
      //     col = col - 1;

      if (this.matrix[row][col].value != 0) {
        count--;
        this.matrix[row][col] = {value: 0, type: 'blank'};
      }
    }
  }

  unUsedInRow(row, num) {
    for (let i = 0; i < this.N; i++) {
      if (this.matrix[row][i].value === num) {
        return false;
      }
    }
    return true;
  }

  unUsedInCol(col, num) {
    for (let i = 0; i < this.N; i++) {
      if (this.matrix[i][col].value === num) {
        return false;
      }
    }
    return true;
  }
}
