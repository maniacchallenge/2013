function() {
	// Filter invalid transactions
	if (this.transactionID == null) {
		return;
	}

	var key = this.transactionID;

	var sourceIP;
	var finalDestinationIP;
	var round;
	var step;
	var completed = false;
	var initialBudget;

	if (this.type == 'X') {	// Initial packet
		round = this.round;
		finalDestinationIP = this.finalDestinationIP;
		initialBudget = this.ceil;

	} else if (this.type == 'A') {	// Advert
		step = {
			"IP": this.sourceIP,
			"gain": null,
			"loss": null,
			"time": this.time,
			"hop": this.deadline
		};
	} else if (this.type == 'W') { // Win
		step = {
			"IP": this.winnerIP,
			"gain": this.winningBid,
			"loss": this.fine,
			"time": this.time,
			"hop": null
		};
	} else if (this.type == 'D') { // Data
		step = {
			"IP": this.sourceIP,
			"gain": null,
			"loss": null,
			"time": this.time,
			"hop": this.deadline
		}
		if (this.destinationIP == "") {
			completed = true;
			if (this.finalDestinationIP != this.me) {
				step.override = false;
			}
		}
	} else {
		return;
	}

	// Create value
	var value = {
		"transactionID": key,
		"sourceIP": sourceIP,
		"finalDestinationIP": finalDestinationIP,
		"round": round,
		"completed": completed,
		"successful": null,
		"steps": [step],
		"initialBudget": initialBudget
	};

	// And emit it
	emit(key, value);
}
