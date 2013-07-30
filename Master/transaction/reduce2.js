
function(key, values) {
	var balanceUpdates = [];

	// Concatnate all 
	for (var i = 0; i < values.length; i++) {
		balanceUpdates = balanceUpdates.concat(values[i].balanceUpdates);
	}

	values[0].balanceUpdates = balanceUpdates;
	return values[0];
}
