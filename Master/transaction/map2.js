
function() {
	for (var i = 0; i < this.steps.length; i++) {
		if (!this.steps[i].completed) {
			continue;
		}

		var amount = 0;
		if (this.steps[i].successful) {
			if (this.steps[i+1] == null) {
				amount = this.steps[i].win;
			} else {
				amount = this.steps[i].win - this.steps[i+1].win;
			}
		} else {
			if (this.steps[i+1] == null) {
				amount = -this.steps[i].fine;
			} else {
				amount = -this.steps[i].fine + this.steps[i+1].fine;
			}			
		}

		var key = this.steps[i].device;
		var value = {
			"device": key,
			"balanceUpdates": {
				"transactionID": this.transactionID,
				"amount": amount
			}
		};

		emit(key, value);
	}
}
