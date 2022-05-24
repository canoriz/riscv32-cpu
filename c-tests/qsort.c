#include <stdio.h>
#define N 11
void qsort(int* arr, int n)
{
    if (n <= 1) return;
    int key = arr[0];
    int j = n-1, i = 0;
    while (1) {
        while (arr[j] >= key && i < j) {
            j--;
        }
        while (arr[i] <= key && i < j) {
            i++;
        }
        if (i >= j) break;

        int t  = arr[i];
        arr[i] = arr[j];
        arr[j] = t;
    }

    // swap
    arr[0] = arr[j];
    arr[j] = key;
    qsort(arr, j);
    qsort(arr+j+1, n-j-1);
}

int main()
{
    int arr[N] = {1,7,3,6,4,2,0,9};
    qsort(arr, N);
    for(int i = 1; i < N ; i++) {
        if (arr[i] < arr[i-1]) {
            return 0;
        }
    }
    return 1;
}