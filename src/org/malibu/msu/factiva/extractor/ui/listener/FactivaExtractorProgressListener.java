package org.malibu.msu.factiva.extractor.ui.listener;

import org.malibu.msu.factiva.extractor.ui.FactivaExtractorProgressToken;

public interface FactivaExtractorProgressListener {
	public void stateChanged(FactivaExtractorProgressToken token);
}
