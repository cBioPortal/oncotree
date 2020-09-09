/*
 * Copyright (c) 2020 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/

package org.mskcc.oncotree.crosswalk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrosswalkStaticResourceParsingException extends Exception {

    private static final Logger logger = LoggerFactory.getLogger(CrosswalkStaticResourceParsingException.class);

    public CrosswalkStaticResourceParsingException() {
        super();
    }

    public CrosswalkStaticResourceParsingException(String message) {
        super(message);
    }

    public CrosswalkStaticResourceParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
