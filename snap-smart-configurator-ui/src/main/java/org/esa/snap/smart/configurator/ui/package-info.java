/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */


/**
 * @author muhammad.bc.
 */


@OptionsPanelController.ContainerRegistration(
        id = "PerformancePreferences",
        categoryName = "#OptionsCategory_Name_Performance",
        iconBase = "org/esa/snap/smart/configurator/ui/Performance32.png",
        keywords = "#OptionsCategory_Keywords_Performance_Optim",
        keywordsCategory = "#OptionsCategory_Keywords_Performance_Prefs",
        position = 3)

@org.openide.util.NbBundle.Messages({
        "OptionsCategory_Name_Performance=Performance",
        "OptionsCategory_Keywords_Performance_Optim=Performance optimization smart configurator",
        "OptionsCategory_Keywords_Performance_Prefs=Performance"
})
package org.esa.snap.smart.configurator.ui;

import org.netbeans.spi.options.OptionsPanelController;

