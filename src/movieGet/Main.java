package movieGet;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class Main extends JFrame {
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JButton btnNewButton;
	private JToolBar toolBar;
	private JTable table;
	private JPanel panel; 
	private JScrollPane scrollPane;
	private JLabel lblPress;
	MovieSerachUtil msu;
	private JTextField textSearch;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Main() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 676, 481);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		setResizable(false);

		toolBar = new JToolBar();
		toolBar.setBounds(6, 6, 162, 20);
		contentPane.add(toolBar);

		btnNewButton = new JButton("一键获取");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				msu = new MovieSerachUtil(Main.this);
				Vector<Vector> rowData = new Vector<Vector>();
				Vector<String> row = null;
				if (msu.checkTime()) {
					// 如果为true说明时间大于三天,则从网页重新爬数据
					//使用子线程来做上网，不会影响主UI的界面，主UI界面不会卡顿
					new Thread() {
						public void run() {
							msu.getLink();
							Map<String, String> map = msu.getDownLoadLinkMap();
							Iterator<Entry<String, String>> it = map.entrySet().iterator();
							while (it.hasNext()) {
								Map.Entry<String, String> entry = (Entry<String, String>) it.next();
								String name = entry.getKey().toString();
								String href = entry.getValue().toString();
								Vector<String> row = new Vector<String>();
								row.add(name);
								row.add(href);
								System.out.println(name + "------" + href);
								rowData.add(row);
							}
							updateTableUi(rowData);

						};
					}.start();

				} else {
					Map<String, String> map = msu.getData();
					Iterator<Entry<String, String>> it = map.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, String> entry = (Entry<String, String>) it.next();
						String name = entry.getKey().toString();
						String href = entry.getValue().toString();
						row = new Vector<String>();
						row.add(name);
						row.add(href);
						System.out.println(name + "------" + href);
						rowData.add(row);
					}
					updateTableUi(rowData);
				}

				//让label不停的做变化，相当于监听器，显示获取进度
				new Thread() {
					public void run() {
						int i = 0;
						while (true) {
							if (table.getRowCount() <= 1) {
								btnNewButton.setText("获取中。。");
								btnNewButton.setEnabled(false);
								if (i < 100) {
									i = msu.getDownLoadLinkMap().size();
									lblPress.setText("进度：" + i + "%");
								}else {
									lblPress.setText("正在整理数据中。。。");
								}
							} else {
								btnNewButton.setText("获取完成");
								btnNewButton.setEnabled(true);
								textSearch.setVisible(true);
								lblPress.setText("");
								break;
							}
						}
					};
				}.start();

			}
		});
		toolBar.add(btnNewButton);

		panel = new JPanel();
		panel.setBounds(6, 38, 664, 415);
		contentPane.add(panel);
		panel.setLayout(null);

		scrollPane = new JScrollPane();
		scrollPane.setBounds(6, 6, 652, 403);
		panel.add(scrollPane);

		table = new JTable();
		scrollPane.setViewportView(table);

		lblPress = new JLabel("");
		lblPress.setBounds(263, 10, 218, 16);
		contentPane.add(lblPress);
		
		textSearch = new JTextField();
		textSearch.setText("search");
		textSearch.setVisible(false);
		textSearch.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				textSearch.setText("");
			}
			@Override
			public void focusLost(FocusEvent e) {
				if(textSearch.getText() == null || textSearch.getText().length() == 0) {
					textSearch.setText("search");
				}
			}
		});
		
		//获取到的电影可以搜索，搜索的内容如果表格中有就会有
		textSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				JTable jt = Main.this.table;
				if(jt != null) {
					String text = textSearch.getText();
					if(text == null || text.length() == 0) {
						((TableRowSorter<TableModel>)jt.getRowSorter()).setRowFilter(null);
					}else {
						if(jt.getRowSorter() != null) {
							((TableRowSorter<TableModel>)jt.getRowSorter()).setRowFilter(RowFilter.regexFilter(text));
						}
					}
				}
			}
		});
		textSearch.setBounds(491, 6, 130, 26);
		contentPane.add(textSearch);
		textSearch.setColumns(10);
		
		
		
	}

	private void updateTableUi(Vector<Vector> datas) {
		Vector<String> columnNames = new Vector<String>();
		columnNames.add("电影名称");
		columnNames.add("下载链接（复制到迅雷中添加新任务）");

		TableModel tableModel = new DefaultTableModel((datas == null || datas.size() == 0) ? null : datas, columnNames);
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);
		table.setModel(tableModel);
		table.setVisible(true);
		table.updateUI();
	}
}
