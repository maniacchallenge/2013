function(key, values) {
	var steps = [];

	// Concatnate all 
	for (var i = 0; i < values.length; i++) {
		steps = steps.concat(values[i].steps);
	}

	values[0].steps = steps;
	return values[0];
}
