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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;

public class Fitness implements EntryPoint {

    final static Constants c = (Constants)GWT.create(Constants.class);
    final Model model = new Model();
    
    public void onModuleLoad() {
        final TabPanel tp = new TabPanel();
        tp.add(new Totals(), c.totals());
        tp.add(new Food(), c.food());
        tp.add(new PA(), c.pA());
        tp.add(new Weight(), c.weight());
        tp.add(new Options(), c.options());
        tp.selectTab(0);
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
            g.setText(0, 0, model.fromDate());
            g.setText(0, 1, c.thru());
            g.setText(0, 2, model.thruDate());
            g.setWidth("100%");
            vp.add(g);

            g = new Grid(7, 2);

            g.setText(0, 0, c.caloriesIn());
            g.setText(0, 1, model.caloriesIn());

            g.setText(1, 0, c.pACalories());
            g.setText(1, 1, model.pACalories());

            g.setText(2, 0, c.metabolism());
            g.setText(2, 1, model.metabolism());

            g.setText(3, 0, c.netCalories());
            g.setText(3, 1, model.netCalories());

            g.setText(4, 0, c.behavioralWeight());
            g.setText(4, 1, model.behavioralWeight());

            g.setText(5, 0, c.daysInRange());
            g.setText(5, 1, model.daysInRange());

            g.setText(6, 0, c.calsLeftToEat());
            g.setText(6, 1, model.calsLeftToEat());

            g.setWidth("100%");
            vp.add(g);
            
            initWidget(vp);
        }
    }

    class Food extends Composite {
        Food() {
            VerticalPanel vp = new VerticalPanel();
            initWidget(vp);
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
            tb.setText(model.metabolismOpt());
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
            tb.setText(model.goalWeight());
            g.setWidget(2, 1, tb);

            g.setText(3, 0, c.goalDeficit());
            tb = new TextBox();
            tb.setText(model.goalDeficit());
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
            tb.setText(model.historyDays());
            g.setWidget(5, 1, tb);

            g.setWidth("100%");
            vp.add(g);

            p = new FlowPanel();
            p.add(new Button(c.oK()));
            p.add(new Button(c.cancel()));
            vp.add(p);

            initWidget(vp);
        }
    }

    static class Model {
        //Totals

        String fromDate() {
            return "Fri 5/7/04";
        }

        String thruDate() {
            return "Fri 5/7/04";
        }

        String caloriesIn() {
            return "1604.0";
        }

        String pACalories() {
            return "150.0";
        }

        String metabolism() {
            return "2032.8";
        }

        String netCalories() {
            return "-578.8";
        }

        String behavioralWeight() {
            return "60.0";
        }

        String daysInRange() {
            return "1.0";
        }

        String calsLeftToEat() {
            return "416.0";
        }


        // Options

        String metabolismOpt() {
            return "11.0";
        }

        String goalWeight() {
            return "170.0";
        }

        String goalDeficit() {
            return "0.0";
        }

        String historyDays() {
            return "90";
        }
    }
}
