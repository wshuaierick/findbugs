
public class DuplicateBranches
{
	public int doit(boolean b, int i, int j)
	{
		int z;
		if (b)
		{
			int k = i * j;
			z = k / 100;
		}
		else
		{
			int k = i * j;
			z = k / 100;
		}
		return z;
	}
	
	public int doit2(int i, int j)
	{
		switch(i)
		{
			case 0:
				j = j + 1;
				if (i < j)
					j = i;
			break;
				
			case 1:
				j = j + 1;
				if (i < j)
					j = i;
			break;
			
			case 2:
				j = j + 1;
				if (i < j)
					j = i;
			break;
			
			default:
				j = j + 1;
				if (i < j)
					j = i;
			break;
		}
		return j;
	}
	
	public int doit3(int i, int j)
	{
		switch(i)
		{
			case 1:
				j = i + 1;
			break;
				
			case 10:
				j = i + 1;
			break;
			
			case 100:
				j = i + 1;
			break;
			
			default:
				j = i + 1;
			break;
		}
		return j;
	}
}
