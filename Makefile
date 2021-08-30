
JDK = /opt/jdk16
JFX = /opt/jfx16

JAR = $(JDK)/bin/jar
JAVA = $(JDK)/bin/java
JAVAC = $(JDK)/bin/javac

# only required for lib/fx-junit.jar
JUNIT = junit/junit-jupiter-api-5.7.jar:junit/apiguardian-api-1.1.jar

classpath = $(subst $(eval) ,:,$(filter %.jar,$1))

default:
	@echo "make [ todo | todo-fxml | todo-fxmlc | todo-jbmlc ]"

# ToDoMVC.fx reference application
todo: lib/ToDo-fx.jar
	$(JAVA) -cp $< -p $(JFX)/lib --add-modules javafx.controls todo.fx.App

# ToDo with fxml
todo-fxml: lib/ToDo-fxml.jar
	$(JAVA) -cp $< -p $(JFX)/lib --add-modules javafx.controls,javafx.fxml todo.fxml.App

# ToDo with compiled fxml
todo-fxmlc: lib/ToDo-fxmlc.jar lib/fx-mvc.jar
	$(JAVA) -cp $(call classpath,$^) -p $(JFX)/lib --add-modules javafx.controls todo.vc.fxml.App

# ToDo with compiled jbml (JSON bean markup)
todo-jbmlc: lib/ToDo-jbmlc.jar lib/fx-mvc.jar
	$(JAVA) -cp $(call classpath,$^) -p $(JFX)/lib --add-modules javafx.controls todo.vc.jbml.App

lib/ToDo-fx.jar: src/ToDo-fx
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -p $(JFX)/lib --add-modules javafx.controls -sourcepath $< $(shell find $< -name '*.java')
	pushd $<; find . -type f -not -name '*.java' -exec cp --parents {} ../../bin/ ';'; popd
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

lib/ToDo-fxml.jar: src/ToDo-fxml
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -p $(JFX)/lib --add-modules javafx.controls,javafx.fxml -sourcepath $< $(shell find $< -name '*.java')
	pushd $<; find . -type f -not -name '*.java' -exec cp --parents {} ../../bin/ ';'; popd
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

lib/ToDo-fxmlc.jar: src/ToDo-fxmlc lib/fx-mvc.jar lib/fx-markup.jar
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -cp $(call classpath,$^) -p $(JFX)/lib --add-modules javafx.controls -sourcepath $< $(shell find $< -name '*.java')
	pushd $<; find . -type f -not -name '*.java' -a -not -name '*.fxml' -exec cp --parents {} ../../bin/ ';'; popd
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

lib/ToDo-jbmlc.jar: src/ToDo-jbmlc lib/fx-mvc.jar lib/fx-markup.jar
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -cp $(call classpath,$^) -p $(JFX)/lib --add-modules javafx.controls -sourcepath $< $(shell find $< -name '*.java')
	pushd $<; find . -type f -not -name '*.java' -a -not -name '*.jbml' -exec cp --parents {} ../../bin/ ';'; popd
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

# fx.mvc annotations and runtime
lib/fx-mvc.jar: src/fx-mvc
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -p $(JFX)/lib --add-modules javafx.controls -sourcepath $< $(shell find $< -name '*.java')
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

# fx view markup annotation processor and compiler
lib/fx-markup.jar: src/fx-markup lib/fx-mvc.jar
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -cp $(call classpath,$^) -p $(JFX)/lib --add-modules javafx.controls -sourcepath $< $(shell find $< -name '*.java')
	cp -a $</META-INF bin
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

# fx junit extension annotations and runtime
lib/fx-junit.jar: src/fx-junit
	@rm -fr bin
	@mkdir -p bin
	$(JAVAC) -d bin -cp $(JUNIT) -p $(JFX)/lib --add-modules javafx.graphics -sourcepath $< $(shell find $< -name '*.java')
	@mkdir -p lib
	$(JAR) -cf $@ -C bin .

clean:
	rm -fr bin
	rm -fr lib

