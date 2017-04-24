package pl.polsl.adbis2017.services;

public class TimeCounter {

	private long sTime, eTime;

	public long getTimeDiff() {
		return eTime - sTime;
	}

	public double getTimeDiffSec() {
		return getTimeDiff() / 1000.0;
	}

	public void saveETime() {
		eTime = System.currentTimeMillis();
	}

	public void saveSTime() {
		sTime = System.currentTimeMillis();
	}

}
