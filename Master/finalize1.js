function(key, value) {

	var steps = {};

	for (var i = 0; i < value.steps.length; i++) {

		
		if (steps[value.steps[i].IP] == null) {
			steps[value.steps[i].IP] = value.steps[i];
		} else {
			// gain
			if (steps[value.steps[i].IP].gain == null) {
				steps[value.steps[i].IP].gain = value.steps[i].gain;
			}

			// loss
			if (steps[value.steps[i].IP].loss == null) {
				steps[value.steps[i].IP].loss = value.steps[i].less;
			}

			// time
			if (steps[value.steps[i].IP].time == null) {
				steps[value.steps[i].IP].time = value.steps[i].time;
			}

			// hop
			if (steps[value.steps[i].IP].hop == null) {
				steps[value.steps[i].IP].hop = value.steps[i].hop;
			}

			// override
			if (value.steps[i].override != null) {
				steps[value.steps[i].IP].override = value.steps[i].override;
			}
		}
	};

	value.steps = [];
	for (var step in steps) {
		if (step == "") {
			continue;
		}
		value.steps.push(steps[step]);
	}

		// Sort values
	value.steps = value.steps.sort(function(a, b) {
		if (a == null || b == null) {
			return 0;
		}
		return b.hop - a.hop;
	});

	if (value.completed) {
		value.successful = false;
	}

	value.max = (value.steps[0].hop - value.steps[value.steps.length-1].hop) * 3000 + 1000;
	value.lag = (value.steps[value.steps.length-1].time - value.steps[0].time);
	value.lgg = (ISODate() - value.steps[0].time);

	if (value.completed && value.max >= value.lag) {
		value.successful = true;
	} else if (value.max < value.lgg) {
		value.completed = true;
		value.successful = false;
	}

	value.sourceIP = value.steps[0].IP;

	return value;
}
