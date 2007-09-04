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
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    static void alert(String message) {
        System.err.println(message);
    }

    static Constants c = (Constants)GWT.create(Constants.class);
    static TabPanel tp;
    static Totals totals;
    static DB food;
    static DB pA;
    static DB weight;
    static Options options;
    
    public void onModuleLoad() {
        totals = new Totals();
        food = new DB(Model.food, 3, new CalRec(c.editFood(), c.foodUnits())); 
        pA = new DB(Model.pA, 3, new CalRec(c.editPA(), c.pAUnits())); 
        weight = new DB(Model.weight, 2, new WeightRec(c.editWeight()));
        options = new Options();

        tp = new TabPanel() {;
            {
                add(Fitness.totals, c.totals());
                add(Fitness.food, c.food());
                add(Fitness.pA, c.pA());
                add(Fitness.weight, c.weight());
                selectTab(0);
            }

            public boolean onBeforeTabSelected(SourcesTabEvents sender,
                                               int tabIndex) {
                Widget tab = getWidget(tabIndex);
                if (tab instanceof DB) {
                    ((DB)tab).update();
                }
                return super.onBeforeTabSelected(sender, tabIndex);
            }
        };
        RootPanel.get().add(tp);
    }

    static class Totals extends Composite {
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
            
            FlowPanel p = new FlowPanel();
            Button b = new Button(c.options());
            b.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    Fitness.options.show();
                }
            });
            p.add(b);
            vp.add(p);
            vp.setCellHorizontalAlignment(p, VerticalPanel.ALIGN_CENTER);

            initWidget(vp);
        }
    }

    static class DB extends Composite {
        final Model.DB db;
        final int nCol;
        final Record record;
        final Grid g;

        DB(Model.DB db, int nCol, Record record) {
            VerticalPanel vp = new VerticalPanel();

            this.db = db;
            this.nCol = nCol;
            this.record = record;
            record.setDB(this);
            record.setModelDB(db);
            g = new Grid(0, nCol);
            g.setWidth("100%");
            vp.add(new ScrollPanel(g));

            FlowPanel p = new FlowPanel();
            Button b = new Button(c.newButton());
            b.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    DB.this.record.show();
                }
            });
            p.add(b);
            vp.add(p);
            vp.setCellHorizontalAlignment(p, VerticalPanel.ALIGN_CENTER);

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

    static class Dialog extends DialogBox {
        final private DockPanel dp;

        Dialog(String title) {
            setText(title);
            
            dp = new DockPanel();
            dp.setSpacing(4);

            FlowPanel p = new FlowPanel();
            Button b = new Button(c.oK());
            b.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    if (ok())
                        hide();
                }
            });
            p.add(b);
            b = new Button(c.cancel());
            b.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    if (cancel())
                        hide();
                }
            });
            p.add(b);
            dp.add(p, DockPanel.SOUTH);
            dp.setCellHorizontalAlignment(p, DockPanel.ALIGN_CENTER);

            setWidget(dp);

            int left = RootPanel.get().getAbsoluteLeft() + 30;
            int top = RootPanel.get().getAbsoluteTop() + 30;
            setPopupPosition(left, top);
        }

        protected void setCenter(Widget center) {
            dp.add(center, DockPanel.CENTER);
        }

        boolean ok() {return true;}

        boolean cancel() {return true;}
    }

    static class Record extends Dialog {
        protected final Grid g = new Grid(2, 2);
        protected final TextBox date = new TextBox();
        protected DB db;
        protected Model.DB mdb;

        Record(String title) {
            super(title);
            g.setText(0, 0, c.date());
            g.setWidget(0, 1, date);
            setCenter(g);
        }

        void setDB(DB db) {this.db = db;}
        void setModelDB(Model.DB mdb) {this.mdb = mdb;}
    }

    static class CalRec extends Record {
        private final TextBox desc = new TextBox();
        private final TextBox quantity = new TextBox();
        private final ListBox unit = new ListBox();
        private final TextBox calPerUnit = new TextBox();

        CalRec(String title, String[] units) {
            super(title);
            g.resizeRows(5);

            g.setText(1, 0, c.desc());
            g.setWidget(1, 1, desc);

            g.setText(2, 0, c.quantity());
            g.setWidget(2, 1, quantity);

            g.setText(3, 0, c.unit());
            for (int i = 0; i < units.length; i++) {
                unit.addItem(units[i]);
            }
            g.setWidget(3, 1, unit);

            g.setText(4, 0, c.calPerUnit());
            g.setWidget(4, 1, calPerUnit);
        }

        boolean ok() {
            try {
                Model.CalRec cr = 
                    new Model.CalRec(date.getText(), desc.getText(), 
                                     Double.parseDouble(quantity.getText()),
                                     Double.parseDouble(calPerUnit.getText()));
                mdb.add(cr);
            }
            catch (NumberFormatException nfe) {
                alert(c.badQuantityOrCalPerUnit());
                return false;
            }
            catch (IllegalArgumentException iae) {
                alert(c.badDate());
                return false;
            }
            db.update();
            return true;
        }
    }

    static class WeightRec extends Record {
        private final TextBox weight = new TextBox();

        WeightRec(String title) {
            super(title);
            g.setText(1, 0, c.weightRec());
            g.setWidget(1, 1, weight);
        }

        boolean ok() {
            try {
                Model.WeightRec wr = 
                    new Model.WeightRec(date.getText(), 
                                        Double.parseDouble(weight.getText()));
                mdb.add(wr);
            }
            catch (NumberFormatException nfe) {
                alert(c.badWeight());
                return false;
            }
            catch (IllegalArgumentException iae) {
                alert(c.badDate());
                return false;
            }
            db.update();
            return true;
        }

    }

    static class Options extends Dialog {
        Options() {
            super(c.options());

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

            setCenter(vp);

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

            Record(String dateStr) {
                this.date = new Date(dateStr); // FIXME: use DateTimeFormat
                                               // FIXME: catch IllegalArgument 
                this.dateStr = // FIXME: use DateTimeFormat
                    "" + (date.getMonth() + 1) + "/" + date.getDate();
            }

            abstract String getField(int index);
        }

        static class CalRec extends Record {
            final String desc;
            final String quantity;
            final String calPerUnit;
            final String calories;

            CalRec(String dateStr, 
                   String desc, double quantity, double calPerUnit) {
                super(dateStr);
                this.desc = desc;
                this.quantity = "" + quantity;
                this.calPerUnit = "" + calPerUnit;
                this.calories = "" + (quantity * calPerUnit);
            }

            String getField(int index) {
                if (index == 0) {return dateStr;}
                if (index == 1) {return desc;}
                if (index == 2) {return calories;}
                return "";
            }

        }

        static class WeightRec extends Record {
            final String weight;

            WeightRec(String dateStr, double weight) {
                super(dateStr);
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
            food.add(new CalRec("5/7/04", "pasta", 14.0, 20.0));

            pA.add(new CalRec("5/7/04", "walking", 0.5, 300.0));

            weight.add(new WeightRec("1/1/04", 90.0));
            weight.add(new WeightRec("4/2/04", 87.0));
            weight.add(new WeightRec("5/7/04", 84.0));
        }

    }
}
