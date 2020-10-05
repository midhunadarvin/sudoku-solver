import React, {PureComponent} from 'react';
import Cell from '../../components/sudoku/cell/Cell';
import {Sudoku} from '../../utils/sudoku';
import {delay} from '../../utils/helpers';
import {Button, Switch, View} from 'react-native';
import {styles} from './Styles';

class SudokuComponent extends PureComponent {
  running = false;
  constructor(props) {
    super(props);
    console.log(props.route.params);
    this.sudoku = new Sudoku(9, props.route.params.grid);

    this.state = {
      matrix: this.sudoku.getMatrix(),
      isEnabled: false,
    };
    this.solve = this.solve.bind(this);
    this.reset = this.reset.bind(this);
    this.toggleSwitch = this.toggleSwitch.bind(this);
  }

  async solve() {
    if (!this.running) {
      this.running = true;
      const animations = this.sudoku.getSteps();

      for (let item of animations) {
        if (!this.running) {
          break;
        }
        let matrix = this.state.matrix.slice();
        matrix[item.row][item.col] = {value: item.value, type: item.type};
        this.setState({matrix});
        await delay(1);
      }
      this.running = false;
    }
  }

  reset() {
    this.running = false;
    this.sudoku.reset();
    this.setState({matrix: this.sudoku.getMatrix()});
  }

  getStyle(i, j, item) {
    let style = [];
    style.push(styles.cell);
    if (item.type) {
      style.push(styles[item.type]);
    }

    if (!(j % 3) && j !== 0) {
      style.push(styles.borderLeft);
    }

    if (!(i % 3) && i !== 0) {
      style.push(styles.borderTop);
    }
    return style;
  }

  toggleSwitch() {
    this.setState({isEnabled: true});
  }

  render() {
    const {matrix, isEnabled} = this.state;
    return (
      <>
        <View style={styles.sudokuContainer}>
          {matrix.map((row, i) => {
            return (
              <View style={styles.row} key={i}>
                {row.map((col, j) => {
                  return (
                    <Cell
                      key={j}
                      style={this.getStyle(i, j, col)}
                      value={col.value}
                    />
                  );
                })}
              </View>
            );
          })}
        </View>
        <View style={styles.container}>
          <Switch
            trackColor={{false: '#767577', true: '#81b0ff'}}
            thumbColor={isEnabled ? '#f5dd4b' : '#f4f3f4'}
            ios_backgroundColor="#3e3e3e"
            onValueChange={this.toggleSwitch}
            value={isEnabled}
          />
        </View>
        <View style={styles.buttonContainer}>
          <Button title="Solve" onPress={this.solve} />
        </View>
        <View style={styles.buttonContainer}>
          <Button title="Reset" onPress={this.reset} />
        </View>
      </>
    );
  }
}

export default SudokuComponent;
