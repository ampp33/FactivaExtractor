package org.malibu.msu.factiva.extractor;

public interface FactivaExtractorProgressListener {
	public void stateChanged(FactivaExtractorProgressToken token);
}
