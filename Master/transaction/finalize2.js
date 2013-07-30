
function(key, value) {
	var balance = 0;

	for (var i=0; i<value.balanceUpdates.length; i++) {
		balance += value.balanceUpdates[i].amount;
	}

	value.balance = balance;
	return value;
}
