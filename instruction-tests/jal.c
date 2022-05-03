int main(int argc)
{
	asm volatile("jal x0, jump");
	asm volatile("ret"); // return if jal failed
	asm volatile("jump:");
	asm volatile("addi gp, x0, 1");
	return 0;
}
