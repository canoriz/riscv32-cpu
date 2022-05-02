int main()
{
	asm volatile("jal x1, jump");
	asm volatile("nop");
	// x5 failed
	asm volatile("addi x5, x0, 1");
	asm volatile("jump:");
	asm volatile("addi x2, x0, 1");
	return 0;
}
