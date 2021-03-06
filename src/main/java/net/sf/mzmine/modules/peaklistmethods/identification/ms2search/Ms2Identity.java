/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.ms2search;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;

public class Ms2Identity extends SimplePeakIdentity {

    public Ms2Identity(final Feature featureA, final Feature featureB, Ms2SearchResult searchResult) {

        super("MS2similarity"+
                        " m/z:" + MZmineCore.getConfiguration().getMZFormat().format(featureB.getMZ()) + 
                        " RT:" + MZmineCore.getConfiguration().getRTFormat().format(featureB.getRT()) + 
                        " Score:" + String.format("%3.1e",searchResult.getScore()) +
                        " NumIonsMatched:" + searchResult.getNumIonsMatched() + 
                        " MatchedIons:"+searchResult.getMatchedIonsAsString());
        
	setPropertyValue(PROPERTY_METHOD, "MS2 search");
    }
}
