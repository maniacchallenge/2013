function(key, value) {
	// Calculate final destination
	for (var i = 0; i < value.steps.length; i++) {
		if (value.steps[i].finalDestinationIP != null) {
			value.finalDestinationIP = value.steps[i].finalDestinationIP;
			break;
		}
	}

	// Merge stations
	var steps = {};
	for (var i = 0; i < value.steps.length; i++) {
		if (steps[value.steps[i].sourceIP] != null) {
			// destinationIP
			if (steps[value.steps[i].sourceIP].destinationIP == null || steps[value.steps[i].sourceIP].destinationIP == "") {
				steps[value.steps[i].sourceIP].destinationIP = value.steps[i].destinationIP;
			}
			// finalDestinationIP
			if (steps[value.steps[i].sourceIP].finalDestinationIP == null) {
				steps[value.steps[i].sourceIP].finalDestinationIP = value.steps[i].finalDestinationIP;
			}

			// deadline
			if (steps[value.steps[i].sourceIP].deadline == null) {
				steps[value.steps[i].sourceIP].deadline = value.steps[i].deadline;
			}

			// ceil
			if (steps[value.steps[i].sourceIP].ceil == null) {
				steps[value.steps[i].sourceIP].ceil = value.steps[i].ceil;
			}

			// fine
			if (steps[value.steps[i].sourceIP].fine == null) {
				steps[value.steps[i].sourceIP].fine = value.steps[i].fine;
			}

			// time
			if (steps[value.steps[i].sourceIP].time == null) {
				steps[value.steps[i].sourceIP].time = value.steps[i].time;
			}
		} else {
			steps[value.steps[i].sourceIP] = value.steps[i];
		}
	};

	// Rebuild array
	value.steps = [];
	for (var s in steps) {
		value.steps.push(steps[s]);
	};

	// Sort values
	value.steps = value.steps.sort(function(a, b) {
		if (a == null || b == null) {
			return 0;
		}
		return b.deadline - a.deadline;
	});

	// Find out if it was successful
	if (value.finalDestinationIP == value.steps[value.steps.length-1].destinationIP) {
		value.successful = true;
	} else {
		value.successful = false;
	}

	// Set the time of the transaction
	value.time = value.steps[value.steps.length-1].time;

	return value;
}
