function(key, values) {
	var value = {
		"transactionID": key,
		"completed": false,
		"steps": []
	};

	// Concatinate all
	for (var i = 0; i < values.length; i++) {
		if (values[i].steps[0] != null) {
			value.steps = value.steps.concat(values[i].steps);
		}

		// Merge values
		if (values[i].souceIP != null) {
			value.sourceIP = values[i].souceIP;
		}

		if (values[i].finalDestinationIP != null) {
			value.finalDestinationIP = values[i].finalDestinationIP;
		}

		if (value.round == null) {
			value.round = values[i].round;
		}

		value.completed = value.completed || values[i].completed;

		if (value.successful == null) {
			value.successful = values[i].successful;
		}

		if (value.initialBudget == null) {
			value.initialBudget = values[i].initialBudget;
		}		
	}

	return value;
}
