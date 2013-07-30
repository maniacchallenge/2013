function(key, values) {
	var value = values[0];

	for (var i = 1; i < values.length; i++) {
		value.balanceUpdates = value.balanceUpdates.concat(values[i].balanceUpdates);
	}

	value.balance = 0;
	for (var i = 0; i < value.balanceUpdates.length; i++) {
		value.balance += value.balanceUpdates[i].amount;
	}

	return value;
}
