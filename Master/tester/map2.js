function() {
	var t = this.value;

	if (t.steps.length < 2) {
		return;
	}

	var amount;
	var i;

	for (i = 1; i < t.steps.length-1; i++) {
		if (t.successful) {
			amount = t.steps[i].gain - t.steps[i+1].gain;
		} else {
			amount = t.steps[i].loss - t.steps[i+1].loss;
		}
		var val = {
			"device": t.steps[i].IP,
			"balance": amount,
			"balanceUpdates": {
				"transactionID": t.transactionID,
				"sourceIP": t.sourceIP,
				"finalDestinationIP": t.finalDestinationIP,
				"amount": amount
			}
		};
		emit(t.steps[i].IP, val);
	}

	if (t.successful) {
		amount = t.steps[i].gain;
	} else {
		amount = t.steps[i].loss;
	}
	if (t.steps[i].override != null) {
		amount -= this.initialBudget;
	}

	var val = {
		"device": t.steps[i].IP,
		"balance": amount,
		"balanceUpdates": {
			"transactionID": t.transactionID,
			"sourceIP": t.sourceIP,
			"finalDestinationIP": t.finalDestinationIP,
			"amount": amount
		}
	};
	emit(t.steps[i].IP, val);
}
