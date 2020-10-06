import java.io.*;
import java.util.*;

public class Main {
    private static final float MIN_SUPPORT = 0.01f;
    private static List<Integer> indices = List.of(0, 1, 2, 3);

    public static void main(String[] args) {
        List<String> transactions = new ArrayList<>();
        List<String> objectsForClassification = new ArrayList<>();
        Set<String> elements = new HashSet<>();

        if (!isDataSuccessfullyPrepared(transactions, elements, objectsForClassification)) {
            return;
        }

        int totalTransactions = transactions.size();

        Map<String, Float> elementsWithSupports = new HashMap<>();
        Map<String, Float> previousElementsWithSupports = new HashMap<>();
        for (String element : elements) {
            previousElementsWithSupports.put(element, null);
        }

        //ITERACJA PRZEJSC K=4 W CELU UZYSKANIA ZBIORU CZĘSTEGO 4-ELEMENTOWEGO
        for (int k = 1; k <= 4; k++) {
            getKElementCommonSets(k, transactions, previousElementsWithSupports, elementsWithSupports, totalTransactions);
            if (elementsWithSupports.isEmpty()) {
                break;
            }
            previousElementsWithSupports = elementsWithSupports;
            elementsWithSupports = new HashMap<>();
        }

        Map<String, Float> finalSetWithSupportValues = new HashMap<>();
        removeRedundantData(previousElementsWithSupports, finalSetWithSupportValues);

        //UTWORZENIE ASOCJACJI
        Map<String, Float> associationsWithConfidenceValues = new HashMap<>();
        createAssociations(transactions, finalSetWithSupportValues, associationsWithConfidenceValues);

        Map<String, Float> finalAssociationsWithConfidenceValues = new HashMap<>();
        removeDuplicates(associationsWithConfidenceValues, finalAssociationsWithConfidenceValues);

        //KLASYFIKACJA
        classifyInputData(objectsForClassification, finalAssociationsWithConfidenceValues);
    }

    private static void classifyInputData(List<String> objectsForClassification, Map<String, Float> finalAssociationsWithConfidenceValues) {
        for (String objectForClassification : objectsForClassification) {
            float classificationValue = 0.0f;
            for (String association : finalAssociationsWithConfidenceValues.keySet()) {
                String[] associationSides = association.split("->");
                String[] leftAssocElements = associationSides[0].split(",");
                boolean associationApplies = true;
                for (String s : leftAssocElements) {
                    if (!objectForClassification.contains(s)) {
                        associationApplies = false;
                    }
                }
                if (associationApplies) {
                    if (associationSides[1].contains("true")) {
                        classificationValue += finalAssociationsWithConfidenceValues.get(association);
                    } else {
                        classificationValue -= finalAssociationsWithConfidenceValues.get(association);
                    }
                }
            }
            if (classificationValue > 0) {
                System.out.println(objectForClassification + " -> " + true);
            } else if (classificationValue < 0) {
                System.out.println(objectForClassification + " -> " + false);
            } else {
                System.out.println(objectForClassification + " -> NIE MOZNA JEDNOZNACZNIE OKRESLIC");
            }
        }
    }

    private static void createAssociations(List<String> transactions, Map<String, Float> finalSetWithSupportValues, Map<String, Float> associationsWithConfidenceValues) {
        for (String elementSet : finalSetWithSupportValues.keySet()) {
            String[] elems = elementSet.split(",");
            int elemsLength = elems.length;
            for (int i = 0; i < elemsLength; i++) {
                int transactionsWithFirstElementGroup = 0;
                int transactionsWithSecondElementGroup = 0;
                for (String transaction : transactions) {
                    if (transaction.contains(elems[i])) {
                        transactionsWithFirstElementGroup++;
                        if (transaction.contains(elems[(i + 1) % elemsLength]) && transaction.contains(elems[(i + 2) % elemsLength]) && transaction.contains(elems[(i + 3) % elemsLength])) {
                            transactionsWithSecondElementGroup++;
                        }
                    }
                }
                float confidence = (float) transactionsWithSecondElementGroup / transactionsWithFirstElementGroup;
                associationsWithConfidenceValues.put(elems[i] + "->" + elems[(i + 1) % elemsLength] + "," + elems[(i + 2) % elemsLength] + "," + elems[(i + 3) % elemsLength], confidence);
            }
            for (int i = 0; i < elemsLength; i++) {
                for (int j = 0; j < elemsLength; j++) {
                    String firstElement = elems[i];
                    String secondElement = elems[j];
                    if (firstElement.equals(secondElement) || associationsWithConfidenceValues.get(secondElement + "," + firstElement) != null) {
                        continue;
                    }
                    int transactionsWithFirstElementGroup = 0;
                    int transactionsWithSecondElementGroup = 0;
                    int[] indices = getTwoElementAssociation(i, j);
                    String third = elems[indices[0]];
                    String fourth = elems[indices[1]];
                    for (String transaction : transactions) {
                        if (transaction.contains(firstElement) && transaction.contains(secondElement)) {
                            transactionsWithFirstElementGroup++;
                            if (transaction.contains(third) && transaction.contains(fourth)) {
                                transactionsWithSecondElementGroup++;
                            }
                        }
                    }
                    float confidence = (float) transactionsWithSecondElementGroup / transactionsWithFirstElementGroup;
                    associationsWithConfidenceValues.put(firstElement + "," + secondElement + "->" + third + "," + fourth, confidence);
                }
            }
            for (int i = 0; i < elemsLength; i++) {
                for (int j = 0; j < elemsLength; j++) {
                    for (int k = 0; k < elemsLength; k++) {
                        String firstElement = elems[i];
                        String secondElement = elems[j];
                        String thirdElement = elems[k];
                        if (firstElement.equals(secondElement) || firstElement.equals(thirdElement) || secondElement.equals(thirdElement)) {
                            continue;
                        }
                        int transactionsWithFirstElementGroup = 0;
                        int transactionsWithSecondElementGroup = 0;
                        int fourthElementIndex = 6 - i - j - k;
                        String fourth = elems[fourthElementIndex];
                        for (String transaction : transactions) {
                            if (transaction.contains(firstElement) && transaction.contains(secondElement) && transaction.contains(thirdElement)) {
                                transactionsWithFirstElementGroup++;
                                if (transaction.contains(fourth)) {
                                    transactionsWithSecondElementGroup++;
                                }
                            }
                        }
                        float confidence = (float) transactionsWithSecondElementGroup / transactionsWithFirstElementGroup;
                        associationsWithConfidenceValues.put(firstElement + "," + secondElement + "," + thirdElement + "->" + fourth, confidence);
                    }
                }
            }
        }
    }

    private static int[] getTwoElementAssociation(Integer first, Integer second) {
        List<Integer> list = new ArrayList<>(indices);
        list.remove(first);
        list.remove(second);
        return new int[]{list.get(0), list.get(1)};
    }

    private static void removeRedundantData(Map<String, Float> K_ElementsWithSupports, Map<String, Float> finalSetWithSupportValues) {
        for (String elements : K_ElementsWithSupports.keySet()) {
            if (!elements.contains("false") && !elements.contains("true")) {
                continue;
            }
            String[] elementsArray = elements.split(",");
            Arrays.sort(elementsArray);
            finalSetWithSupportValues.put(elementsArray[0] + "," + elementsArray[1] + "," + elementsArray[2] + "," + elementsArray[3], K_ElementsWithSupports.get(elements));
        }
    }

    private static void removeDuplicates(Map<String, Float> fourElementsWithSupports, Map<String, Float> finalSetWithSupportValues) {
        for (String fourElements : fourElementsWithSupports.keySet()) {
            String[] elementsArray = fourElements.split("->");
            if (elementsArray[0].contains("false") || elementsArray[0].contains("true") || (!elementsArray[1].contains("false") && !elementsArray[1].contains("true"))) {
                continue;
            }
            String[] leftAssocElements = elementsArray[0].split(",");
            String[] rightAssocElements = elementsArray[1].split(",");
            Arrays.sort(leftAssocElements);
            Arrays.sort(rightAssocElements);
            StringBuilder leftString = new StringBuilder();
            for (int i = 0; i < leftAssocElements.length; i++) {
                leftString.append(leftAssocElements[i]);
                if (i != leftAssocElements.length - 1) {
                    leftString.append(",");
                }
            }
            StringBuilder rightString = new StringBuilder();
            for (int i = 0; i < rightAssocElements.length; i++) {
                rightString.append(rightAssocElements[i]);
                if (i != rightAssocElements.length - 1) {
                    rightString.append(",");
                }
            }
            finalSetWithSupportValues.put(leftString.toString() + "->" + rightString.toString(), fourElementsWithSupports.get(fourElements));
        }
    }

    private static void getKElementCommonSets(int k, List<String> transactions, Map<String, Float> previousElementsWithSupports, Map<String, Float> K_ElementsWithSupports, int totalTransactions) {
        Set<String> kCommonElements = previousElementsWithSupports.keySet();
        Set<String> commonElements = new HashSet<>();

        if (k == 1) {
            for (String element : kCommonElements) {
                int elementTransactions = 0;
                for (String transaction : transactions) {
                    if (transaction.contains(element)) {
                        elementTransactions++;
                    }
                }

                float support = (float) elementTransactions / totalTransactions;
                if (support >= MIN_SUPPORT) {
                    K_ElementsWithSupports.put(element, support);
                }
            }
        } else {
            for (String s : kCommonElements) {
                if (s.contains(",")) {
                    commonElements.addAll(Arrays.asList(s.split(",")));
                } else {
                    commonElements.add(s);
                }
            }

            for (String elementSet : kCommonElements) {
                for (String anotherElement : commonElements) {
                    if (elementSet.contains(anotherElement)) {
                        continue;
                    }

                    int transactionsWithKElements = 0;
                    String[] splitSet = elementSet.split(",");
                    boolean areAllElementsInTransaction = true;

                    for (String transaction : transactions) {
                        if (transaction.contains(anotherElement)) {
                            for (String element : splitSet) {
                                if (!transaction.contains(element)) {
                                    areAllElementsInTransaction = false;
                                }
                            }

                            if (areAllElementsInTransaction) {
                                transactionsWithKElements++;
                            }
                        }
                        areAllElementsInTransaction = true;
                    }

                    float support = (float) transactionsWithKElements / totalTransactions;
                    if (support >= MIN_SUPPORT) {
                        StringBuilder sb = new StringBuilder();
                        for (String element : splitSet) {
                            sb.append(element);
                            sb.append(",");
                        }
                        sb.append(anotherElement);
                        K_ElementsWithSupports.put(sb.toString(), support);
                    }
                }
            }
        }
    }

    private static boolean isDataSuccessfullyPrepared(List<String> transactions, Set<String> elements, List<String> objectsForClassification) {
        File file = new File("klasyfikacja.txt");
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            return false;
        }

        BufferedReader reader = new BufferedReader(fileReader);

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] attributes = line.split(" ");
                if (attributes.length == 8) {
                    transactions.add(attributes[2] + "," + attributes[3] + "," + attributes[4] + "," + attributes[5] + "," + attributes[6] + "," + attributes[7]);
                } else {
                    objectsForClassification.add(line);
                }
            }

            for (String transaction : transactions) {
                String[] elementsInTransaction = transaction.split(",");
                elements.addAll(Arrays.asList(elementsInTransaction));
            }

        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
