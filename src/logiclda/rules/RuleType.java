package logiclda.rules;

/**
 * Different types of logic rule
 * @author andrzeje
 */
public enum RuleType {

	SEED("SEED"),
	SENTEXCL("SENTEXCL"),
	SENTINCL("SENTINCL");
	
	private final String ruleName;
	
	RuleType(String name)
	{
		this.ruleName = name;		
	}
		
	public boolean matchesName(String str)
	{
		return this.ruleName.equalsIgnoreCase(str);
	}
}
