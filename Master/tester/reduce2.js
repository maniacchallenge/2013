function(key, values) {
	var value = values[0];

	for (var i = 1; i < values.length; i++) {
		value.balanceUpdates.concat(values[i].balanceUpdates);
	}

	value.amount = 0;
	for (var i = 0; i < value.balanceUpdates.length; i++) {
		value.amount += value.balanceUpdates[i].amount;
	}

	return value;
}
