function() {
	var key = this.transactionID;

	// Filter invalid transactions
	if (key == null) {
		return;
	}

	// Create station
	var object = {
		"sourceIP":           this.sourceIP,
		"destinationIP":      this.destinationIP,
		"finalDestinationIP": this.finalDestinationIP,
		"deadline":           this.deadline,
		"ceil":               this.ceil,
		"fine":               this.fine,
		"time":               this.time,
		"round":              this.round
	}

	// Create value
	var value = {
		"transactionID": key,
		"steps": [object]
	}

	// And emit it
	emit(key, value);
}
