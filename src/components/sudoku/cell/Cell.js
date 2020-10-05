import React from 'react';
import {Text, View} from 'react-native';

export function Cell(props) {
  return (
    <View style={props.style}>
      <Text>{props.value > 0 ? props.value : ''}</Text>
    </View>
  );
}

export default Cell;
