package logiclda.infer;

public enum InferScheme 
{
	LDA("LDA"), // standard LDA with Collapsed Gibbs (LDA only)
	MWS("MWS"), // logic MaxWalkSAT (Logic only)
	CGS("CGS"), // LogicLDA Collapsed Gibbs (LDA+Logic)
	MPL("MPL"), // MaxWalkSAT+LDA (LDA+Logic)
	MIR("MIR"); // Mirror Descent (LDA+Logic)	
	
	private final String schemeName;
	
	InferScheme(String name)
	{
		this.schemeName = name;		
	}
		
	public boolean matchesName(String str)
	{
		return this.schemeName.equalsIgnoreCase(str);
	}
		
}
