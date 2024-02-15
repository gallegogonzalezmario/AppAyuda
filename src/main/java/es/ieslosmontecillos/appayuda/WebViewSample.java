package es.ieslosmontecillos.appayuda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import netscape.javascript.JSObject;

public class WebViewSample extends Application
{
    private Scene scene;
    @Override
    public void start(Stage stage)
    {
        // create the scene
        stage.setTitle("Web View");
        scene = new Scene(new Browser(),750,500, Color.web("#666970"));
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("css/BrowserToolbar.css").toExternalForm());
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }

    class Browser extends Region
    {
        private HBox toolBar;
        private static String[] imageFiles = new String[]{
                "images/moodle.jpg",
                "images/facebook.jpg",
                "images/twitter.jpg",
                "images/montecillos.png",
                "images/help.png"
        };

        private static String[] captions = new String[]{
                "Moodle",
                "Facebook",
                "Twitter",
                "IES Los Montecillos",
                "Help"
        };

        private static String[] urls = new String[]{
                "https://moodle.org/",
                "https://www.facebook.com/",
                "https://x.com/",
                "https://www.ieslosmontecillos.es/wp/",
                WebViewSample.class.getResource("help.html").toExternalForm()
        };
        final ImageView selectedImage = new ImageView();
        final Hyperlink[] hpls = new Hyperlink[captions.length];
        final Image[] images = new Image[imageFiles.length];

        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();

        final Button toggleHelpTopics = new Button("Toggle Help Topics");
        private boolean needDocumentationButton = false;

        // Pop-up window
        final WebView smallView = new WebView();

        // ComboBox for the history
        final ComboBox comboBox = new ComboBox();

        public Browser()
        {
            //apply the styles
            getStyleClass().add("browser");
            //Para tratar lo cuatro enlaces
            for (int i = 0; i < captions.length; i++)
            {
                Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
                Image image = images[i] = new Image(getClass().getResourceAsStream(imageFiles[i]));
                hpl.setGraphic(new ImageView (image));
                final String url = urls[i];
                final boolean addButton = (hpl.getText().equals("Help"));

                //proccess event
                hpl.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e)
                    {
                        needDocumentationButton = addButton;
                        webEngine.load(url);
                    }
                });
            }

            // Configure ComboBox for the history
            comboBox.setPrefWidth(60);

            // load the web page
            webEngine.load("https://www.ieslosmontecillos.es/wp/");

            //create the toolbar
            toolBar = new HBox();
            toolBar.setAlignment(Pos.CENTER);
            toolBar.getStyleClass().add("browser-toolbar");
            toolBar.getChildren().addAll(hpls);
            toolBar.getChildren().add(createSpacer());
            toolBar.getChildren().add(comboBox);

            //set action for the button
            toggleHelpTopics.setOnAction(new EventHandler() {
                @Override
                public void handle(Event t)
                {
                    webEngine.executeScript("toggle_visibility('help_topics')");
                }
            });

            smallView.setPrefSize(120, 80);

            // handle popup windows
            webEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
                @Override
                public WebEngine call(PopupFeatures config) {
                    smallView.setFontScale(0.8);
                    if(!toolBar.getChildren().contains(smallView))
                        toolBar.getChildren().add(smallView);
                    return smallView.getEngine();
                }
            });

            // History
            final WebHistory history = webEngine.getHistory();

            // Process history

            // Keeps the ComboBox updated with URLs of the navigation history
            history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {
                @Override
                public void onChanged(Change<? extends WebHistory.Entry> c) {
                    c.next();

                    for (WebHistory.Entry e : c.getRemoved())
                        comboBox.getItems().remove(e.getUrl());
                    for (WebHistory.Entry e : c.getAddedSubList())
                        comboBox.getItems().add(e.getUrl());
                }
            });

            //set the behavior for the history combobox
            comboBox.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent ev) {
                    int offset = comboBox.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();
                    history.go(offset);
                }
            });


            // process page loading
            webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> ov, Worker.State
                        oldState, Worker.State newState)
                {
                    toolBar.getChildren().remove(toggleHelpTopics);
                    if (newState == Worker.State.SUCCEEDED)
                    {
                        JSObject win = (JSObject) webEngine.executeScript("window");
                        win.setMember("app", new JavaApp());

                        if (needDocumentationButton)
                            toolBar.getChildren().add(toggleHelpTopics);
                    }
                }
            });

            //add components
            getChildren().add(toolBar);
            getChildren().add(browser);
        }

        // JavaScript interface object
        public class JavaApp
        {
            public void exit(){Platform.exit();}
        }

        private Node createSpacer()
        {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            return spacer;
        }

        @Override
        protected void layoutChildren()
        {
            double w = getWidth();
            double h = getHeight();
            double tbHeight = toolBar.prefHeight(w);
            layoutInArea(browser,0,0,w,h-tbHeight,0, HPos.CENTER, VPos.CENTER);
            layoutInArea(toolBar,0,h-
                    tbHeight,w,tbHeight,0,HPos.CENTER,VPos.CENTER);
        }

        @Override
        protected double computePrefWidth(double height)
        {
            return 750;
        }

        @Override
        protected double computePrefHeight(double width)
        {
            return 500;
        }
    }
}