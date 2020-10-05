import {Dimensions, StyleSheet} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';

export const styles = StyleSheet.create({
  sudokuContainer: {
    height: Dimensions.get('window').height * 0.6,
    // flex: 1,
    // flexDirection: 'column',
    // flex-direction: column,
    // overflow: hidden,
    // box-shadow: 0px 0px 5px 5px #bdc3c7;
  },
  row: {
    flex: 1,
    flexDirection: 'row',
  },
  cell: {
    flex: 1,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  blank: {
    borderRightColor: '#bdc3c7',
    borderLeftColor: '#bdc3c7',
    borderBottomColor: '#bdc3c7',
    borderTopColor: '#bdc3c7',
    backgroundColor: 'white',
  },
  fix: {
    color: '#7f8c8d',
    backgroundColor: '#ecf0f1',
  },
  red: {
    borderColor: 'red',
  },
  green: {
    borderColor: 'green',
  },
  borderLeft: {
    borderLeftWidth: 2,
    borderLeftColor: '#34495e',
  },
  borderTop: {
    borderTopWidth: 2,
    borderTopColor: '#34495e',
  },
  buttonContainer: {
    margin: 15,
  },
});
