/*
Copyright 2007 Ofer Barkai

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fitness.client;

public interface Constants extends com.google.gwt.i18n.client.Constants {
    // Totals
    String totals();
    String showTotalsFor();
    String today();
    String yesterday();
    String thisWeek();
    String thisMonth();
    String allData();
    String thru();
    String caloriesIn();
    String pACalories();
    String metabolism();
    String netCalories();
    String behavioralWeight();
    String daysInRange();
    String calsLeftToEat();
    String food();
    String pA();
    String weight();

    // Options
    String options();
    String metabolismOpt();
    String goal();
    String weightOptVal();
    String deficit();
    String goalWeight();
    String goalDeficit();
    String weightOpt();
    String pounds();
    String kilograms();
    String historyDays();
    String oK();
    String cancel();

    // Records
    String newButton();
    String editFood();
    String editPA();
    String date();
    String desc();
    String quantity();
    String unit();
    String[] foodUnits();
    String[] pAUnits();
    String calPerUnit();
    String editWeight();
    String weightRec();

    String badDate();
    String badQuantityOrCalPerUnit();
    String badWeight();

}
