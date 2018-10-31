#include <stdio.h>
#include <unistd.h>

char line[1024];

int main() {
	int line_ctr = 0;
	while(fgets(line, sizeof(line), stdin) != NULL) {
		++line_ctr;
		if(line[0] == '#')
			continue;
		double arr[4];
		int nr;
		if((nr = sscanf(line, " %lf %lf %lf %lf", arr, arr + 1, arr + 2, arr + 3)) != 4) {
			if(nr >= 1) {
				fprintf(stderr, "Missing numbers on line %d: read only %d numbers\n", line_ctr, nr);
			}
			continue;
		}
		fprintf(stderr, "%f %f %f %f\n", arr[0], arr[1], arr[2], arr[3]);
		write(1, arr, sizeof(arr));
	}
	return 0;
}
