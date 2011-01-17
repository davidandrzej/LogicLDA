package logiclda.eval;

public class GeneralizationResult 
{
	double ldaobj;
	double logicobj;
	public int[] finalSample;
	
	public GeneralizationResult() {}

	public double getLdaobj() {
		return ldaobj;
	}

	public void setLdaobj(double ldaobj) {
		this.ldaobj = ldaobj;
	}

	public double getLogicobj() {
		return logicobj;
	}

	public void setLogicobj(double logicobj) {
		this.logicobj = logicobj;
	}

	public int[] getFinalSample() {
		return finalSample;
	}

	public void setFinalSample(int[] finalSample) {
		this.finalSample = finalSample;
	}	
}
