package logiclda;

/**
 * Different types of side information
 * @author andrzeje
 */
public enum SideInfoType {

	SENTENCE("sent");
	
	public final String infoName;
	
	SideInfoType(String name)
	{
		this.infoName = name;
	}
		
	public boolean matches(String str)
	{
		return this.infoName.equalsIgnoreCase(str);
	}
}
