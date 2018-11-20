#include <stdio.h>

int main() {
	double mat1[4][4];
	double mat2[4][4];
	double res[4][4];
	printf("A = \n");
	for(int i = 0; i < 4; ++i) {
		for(int j = 0; j < 4; ++j) {
			scanf(" %lf", &mat1[i][j]);
		}
	}
	printf("B = \n");
	for(int i = 0; i < 4; ++i) {
		for(int j = 0; j < 4; ++j) {
			scanf(" %lf", &mat2[i][j]);
		}
	}

	printf("AB = \n");
	for(int i = 0; i < 4; ++i) {
		for(int j = 0; j < 4; ++j) {
			double sum = 0;
			for(int k = 0; k < 4; ++k) {
				sum += mat1[i][k] * mat2[k][j];
			}
			printf("%.16g ", sum);
		}
		printf("\n");
	}
	return 0;
}
