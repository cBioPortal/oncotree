/*
 * Copyright (c) 2017 - 2020 Memorial Sloan-Kettering Cancer Center.
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

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.mskcc.oncotree.crosswalk.CrosswalkStaticResourceParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Manda Wilson
 **/
@Repository
public class CrosswalkRepository {

    private static final Logger logger = LoggerFactory.getLogger(CrosswalkRepository.class);

    private static final String STATIC_CROSSWALK_FILENAME = "staticCrosswalkOncotreeMappings.txt";
    private Map<String, MSKConcept> parsedStaticResource = null;

    public MSKConcept getByOncotreeCode(String oncotreeCode) {
        parseCrosswalkResourceFileIfNeeded();
        MSKConcept concept = parsedStaticResource.get(oncotreeCode);
        return concept;
    }

    private void parseCrosswalkResourceFileIfNeeded() {
        if (parsedStaticResource == null) {
            parsedStaticResource = new HashMap<String, MSKConcept>();
            try {
                parseCrosswalkResourceFile();
            } catch (CrosswalkStaticResourceParsingException e) {
                logger.error(e.toString());
                parsedStaticResource.clear();
                logger.error("external reference map empty following parsing error");
            }
        }
    }

    private void parseCrosswalkResourceFile() throws CrosswalkStaticResourceParsingException {
        try {
            Resource resource = new ClassPathResource(STATIC_CROSSWALK_FILENAME);
            InputStream inputStream = resource.getInputStream();
            InputStreamReader isreader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(isreader);
            while (reader.ready()) {
                String line = reader.readLine();
                String columns[] = line.split("\t");
                if (columns.length != 3) {
                    throw new CrosswalkStaticResourceParsingException("could not parse static file with crosswalk mappings - wrong column count");
                }
                String code = columns[0];
                String nci[] = columns[1].split(",");
                String umln[] = columns[2].split(",");
                MSKConcept concept = new MSKConcept();
                List<String> oncotreeCodes = new ArrayList<>();
                oncotreeCodes.add(code);
                concept.setOncotreeCodes(oncotreeCodes);
                concept.setConceptIds(Arrays.asList(umln));
                HashMap<String, List<String>> crosswalks = new HashMap<String, List<String>>();
                crosswalks.put("NCI", Arrays.asList(nci));
                concept.setCrosswalks(crosswalks);
                parsedStaticResource.put(code, concept);
            }
        } catch (IOException e) {
            throw new CrosswalkStaticResourceParsingException("error : could not parse static file with crosswalk mappings");
        }
    }

}

