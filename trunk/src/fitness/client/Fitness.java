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

import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    final static Constants c = (Constants)GWT.create(Constants.class);
    
    public void onModuleLoad() {
        final TabPanel tp = new TabPanel() {;
            {
                add(new Totals(), c.totals());
                add(new Table(Model.food, 3), c.food());
                add(new Table(Model.pA, 3), c.pA());
                add(new Table(Model.weight, 2), c.weight());
                add(new Options(), c.options());
                selectTab(0);
            }

            public boolean onBeforeTabSelected(SourcesTabEvents sender,
                                               int tabIndex) {
                Widget tab = getWidget(tabIndex);
                if (tab instanceof Table) {
                    ((Table)tab).update();
                }
                return super.onBeforeTabSelected(sender, tabIndex);
            }
        };
        RootPanel.get().add(tp);
    }

    class Totals extends Composite {
        Totals() {
            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(1, 2);
            g.setText(0, 0, c.showTotalsFor());
            ListBox lb = new ListBox();
            lb.addItem(c.today());
            lb.addItem(c.yesterday());
            lb.addItem(c.thisWeek());
            lb.addItem(c.thisMonth());
            lb.addItem(c.allData());
            g.setWidget(0, 1, lb);
            g.setWidth("100%");
            vp.add(g);

            g = new Grid(1, 3);
            g.setText(0, 0, Model.fromDate());
            g.setText(0, 1, c.thru());
            g.setText(0, 2, Model.thruDate());
            g.setWidth("100%");
            vp.add(g);

            g = new Grid(7, 2);

            g.setText(0, 0, c.caloriesIn());
            g.setText(0, 1, Model.caloriesIn());

            g.setText(1, 0, c.pACalories());
            g.setText(1, 1, Model.pACalories());

            g.setText(2, 0, c.metabolism());
            g.setText(2, 1, Model.metabolism());

            g.setText(3, 0, c.netCalories());
            g.setText(3, 1, Model.netCalories());

            g.setText(4, 0, c.behavioralWeight());
            g.setText(4, 1, Model.behavioralWeight());

            g.setText(5, 0, c.daysInRange());
            g.setText(5, 1, Model.daysInRange());

            g.setText(6, 0, c.calsLeftToEat());
            g.setText(6, 1, Model.calsLeftToEat());

            g.setWidth("100%");
            vp.add(g);
            
            initWidget(vp);
        }
    }

    class Table extends Composite {
        final Model.DB db;
        final int nCol;
        final Grid g;

        Table(Model.DB db, int nCol) {
            VerticalPanel vp = new VerticalPanel();

            FlowPanel p = new FlowPanel();
            p.add(new Button(c.newButton()));
            vp.add(p);
            vp.setCellHorizontalAlignment(
                p, HasHorizontalAlignment.ALIGN_CENTER);

            this.db = db;
            this.nCol = nCol;
            g = new Grid(0, nCol);
            g.setWidth("100%");
            vp.add(new ScrollPanel(g));

            initWidget(vp);
        }

        void update() {
            g.resizeRows(db.size());
            int i = 0;
            for (Iterator it = db.iterator(); it.hasNext(); i++) {
                Model.Record r = (Model.Record)it.next();
                for (int j = 0; j < nCol; j++) {
                    g.setText(i, j, r.getField(j));
                }
            }
        }
    }

    class PA extends Composite {
        PA() {
            VerticalPanel vp = new VerticalPanel();
            initWidget(vp);
        }
    }

    class Weight extends Composite {
        Weight() {
            VerticalPanel vp = new VerticalPanel();
            initWidget(vp);
        }
    }

    class Options extends Composite {
        Options() {
            VerticalPanel vp = new VerticalPanel();
            Grid g = new Grid(6, 2);

            g.setText(0, 0, c.metabolismOpt());
            TextBox tb = new TextBox();
            tb.setText(Model.metabolismOpt());
            g.setWidget(0, 1, tb);

            g.setText(1, 0, c.goal());
            FlowPanel p = new FlowPanel();
            RadioButton rb = new RadioButton(c.goal(), c.weightOptVal());
            p.add(rb);
            rb.setChecked(true); // FIXME: should be in the model
            rb = new RadioButton(c.goal(), c.deficit());
            p.add(rb);
            g.setWidget(1, 1, p);

            g.setText(2, 0, c.goalWeight());
            tb = new TextBox();
            tb.setText(Model.goalWeight());
            g.setWidget(2, 1, tb);

            g.setText(3, 0, c.goalDeficit());
            tb = new TextBox();
            tb.setText(Model.goalDeficit());
            g.setWidget(3, 1, tb);

            g.setText(4, 0, c.weightOpt());
            p = new FlowPanel();
            rb = new RadioButton(c.weightOpt(), c.pounds());
            p.add(rb);
            rb = new RadioButton(c.weightOpt(), c.kilograms());
            p.add(rb);
            rb.setChecked(true); // FIXME: should be in the model
            g.setWidget(4, 1, p);

            g.setText(5, 0, c.historyDays());
            tb = new TextBox();
            tb.setText(Model.historyDays());
            g.setWidget(5, 1, tb);

            g.setWidth("100%");
            vp.add(g);

            p = new FlowPanel();
            p.add(new Button(c.oK()));
            p.add(new Button(c.cancel()));
            vp.add(p);
            vp.setCellHorizontalAlignment(
                p, HasHorizontalAlignment.ALIGN_CENTER);

            initWidget(vp);
        }
    }

    static class Model {
        //Totals
        static String fromDate() {
            return "Fri 5/7/04";
        }

        static String thruDate() {
            return "Fri 5/7/04";
        }

        static String caloriesIn() {
            return "1604.0";
        }

        static String pACalories() {
            return "150.0";
        }

        static String metabolism() {
            return "2032.8";
        }

        static String netCalories() {
            return "-578.8";
        }

        static String behavioralWeight() {
            return "60.0";
        }

        static String daysInRange() {
            return "1.0";
        }

        static String calsLeftToEat() {
            return "416.0";
        }


        // Options
        static String metabolismOpt() {
            return "11.0";
        }

        static String goalWeight() {
            return "170.0";
        }

        static String goalDeficit() {
            return "0.0";
        }

        static String historyDays() {
            return "90";
        }

        // Records
        static abstract class Record {
            final Date date;
            final String dateStr;

            Record(Date date) {
                this.date = date;
                this.dateStr = 
                    "" + (date.getMonth() + 1) + "/" + date.getDate();
            }

            abstract String getField(int index);
        }

        static class CalRec extends Record {
            final String desc;
            final String quantity;
            final String unit;
            final String calPerUnit;
            final String calories;

            CalRec(Date date, String desc, double quantity, 
                   Unit unit, double calPerUnit) {
                super(date);
                this.desc = desc;
                this.quantity = "" + quantity;
                this.unit = unit.name;
                this.calPerUnit = "" + calPerUnit;
                this.calories = "" + (quantity * calPerUnit);
            }

            String getField(int index) {
                if (index == 0) {return dateStr;}
                if (index == 1) {return desc;}
                if (index == 2) {return calories;}
                return "";
            }

            static class Unit {
                final static Unit OUNCE = new Unit(c.ounce());
                final static Unit HOUR = new Unit(c.hour());
                final String name;
                private Unit(String name) {this.name = name;}
            }

        }

        static class WeightRec extends Record {
            final String weight;

            WeightRec(Date date, double weight) {
                super(date);
                this.weight = "" + weight;
            }

            String getField(int index) {
                if (index == 0) {return dateStr;}
                if (index == 1) {return weight;}
                return "";
            }
        }

        static class DB extends ArrayList {
        }

        final static DB food = new DB();
        final static DB pA = new DB();
        final static DB weight = new DB();

        static {
            food.add(new CalRec(
                new Date("5/7/04"), "pasta", 14.0, CalRec.Unit.OUNCE, 20.0));

            pA.add(new CalRec(
                new Date("5/7/04"), "walking", 0.5, CalRec.Unit.HOUR, 300.0));

            weight.add(new WeightRec(new Date("1/1/04"), 90.0));
            weight.add(new WeightRec(new Date("4/2/04"), 87.0));
            weight.add(new WeightRec(new Date("5/7/04"), 84.0));
        }

    }
}
